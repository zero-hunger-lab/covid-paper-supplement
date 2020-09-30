"""Fit virus stage transition matrix for COVID-19 simulation."""

__author__ = 'Ruud Brekelmans'
__email__ = 'r.c.m.brekelmans@tilburguniversity.edu'

import argparse
from abc import ABC, abstractmethod
from pathlib import Path
from typing import List, Mapping

import numpy as np

import pandas as pd

from scipy.linalg import block_diag
from scipy.optimize import Bounds, LinearConstraint, minimize
from scipy.spatial import distance_matrix

# Maximum number of iteration for converging Markov chain.
MAX_ITER = 5000

# All health stages as ID
STAGES = {
    0: 'HEALTHY',
    1: 'INFECTED_NOSYMPTOMS_NOTCONTAGIOUS',
    2: 'INFECTED_NOSYMPTOMS_ISCONTAGIOUS',
    3: 'INFECTED_SYMPTOMS_MILD',
    4: 'INFECTED_SYMPTOMS_SEVERE_ICpossible',
    5: 'INFECTED_SYMPTOMS_SEVERE_ICnotpossible',
    6: 'INFECTED_SYMPTOMS_SEVERE_QUEUE',
    7: 'CURED',
    8: 'DEAD',
}

# Inverse lookup health stages
STAGE_ID = {val: key for key, val in STAGES.items()}

# Entries in transition matrix that have fixed values.
FIXED_TRANSITIONS = {
    (0, 1): 1.0,  # artificial, real transition determined by simulation
    (6, 6): 1.0,
    (7, 7): 1.0,
    (8, 8): 1.0,
}

# Entries in transition matrix that are unknown and need to be estimated.
UNKNOWN_TRANSITIONS = [
    (1, 2), (1, 7),
    (2, 3), (2, 7),
    (3, 4), (3, 5), (3, 7),
    (4, 7), (4, 8),
    (5, 7), (5, 8),
]


def stage_ids(stages) -> set:
    """Return the stage IDs for  `stages` in a set.

    Arguments
    ---------
    stages: str or iterable of str
        One stage, or a comma separated string of stages, or an iterable of
        strings of stages.
    """
    if isinstance(stages, str):
        if ',' in stages:
            stages = [stage.strip() for stage in stages.split(',')]
        else:
            stages = [stages]
    return {STAGE_ID[stage] for stage in stages}


def post_process_transition_matrix(transitions: np.ndarray) -> np.ndarray:
    """Copy transitions for ISQ.

    Transition probabilities for `ISQ` are identical to `ISN`.
    """
    isn = STAGE_ID['INFECTED_SYMPTOMS_SEVERE_ICnotpossible']
    isq = STAGE_ID['INFECTED_SYMPTOMS_SEVERE_QUEUE']
    transitions[isq] = transitions[isn]
    transitions[isq, isq] = transitions[isn, isn]
    transitions[isq, isn] = 0.0
    return transitions


def fill_transition_matrix(transitions):
    """Make transition matrix from given transitions dictionary.

    Diagonal entries are automatically adapted to yield row-1 sums. However,
    there is no check whether all entries are non-negative.
    """
    # Initialize zero matrix and fill the fixed and other transitions.
    n = len(STAGES)
    transition_matrix = np.zeros((n, n), dtype=float)
    for key, fixed_val in FIXED_TRANSITIONS.items():
        transition_matrix[key] = fixed_val
    for key, val in transitions.items():
        transition_matrix[key] = val

    # Make sure rows add to one by deriving diagonal values.
    for i, row in enumerate(transition_matrix):
        row_sum = row.sum() - row[i]
        # if row_sum > 1.:
        #    raise ValueError('Row sum cannot exceed 1.')
        row[i] = 1 - row_sum

    transition_matrix = post_process_transition_matrix(transition_matrix)
    return transition_matrix


def vector_to_transition_matrix(x):
    """Make transition matrix with transitions from a vector.

    Assumes transitions are in same order as `UNKNOWN_TRANSITIONS`.
    """
    if len(x) < len(UNKNOWN_TRANSITIONS):
        raise ValueError('Not enough parameters.')
    return fill_transition_matrix(
        {key: val for key, val in zip(UNKNOWN_TRANSITIONS, x)})


class Markov:
    """Markov chain."""

    eps = 1e-6
    unit_of_time = 0.5  # one period is half a day

    def __init__(self, transition_matrix):
        self.transition_matrix = transition_matrix
        self.n = transition_matrix.shape[0]

    def is_absorbing(self, state) -> bool:
        """Return whether state is absorbing or not."""
        return self.transition_matrix[state, state] == 1.

    def absorbing_states(self) -> set:
        """Return set of absorbing states."""
        return {
            stage for stage in range(self.n)
            if self.is_absorbing(stage)
        }

    def transient_states(self) -> set:
        """Return set of transient states."""
        return set(range(self.n)).difference(self.absorbing_states())

    def is_transient(self, state: int) -> bool:
        """Return whether state is transient."""
        return self.transition_matrix[state, state] < 1

    def set_transition_matrix(self, transition_matrix):
        """Update transition matrix."""
        self.transition_matrix = transition_matrix

    def convergence(self, start=None, max_iter=MAX_ITER):
        """Compute probabilities until convergence."""
        if start is None:
            start = np.zeros(self.n)
            start[0] = 1
        current = start
        x = [current]
        for _ in range(max_iter):
            current = current @ self.transition_matrix
            x.append(current)
            if np.linalg.norm(start - current) < self.eps:
                break
            start = current
        else:
            print('Iteration limit reached.')
        return np.array(x)

    def mean_time(self) -> dict:
        """Compute mean time spent in transient stages.

        Returns
        -------
        dict
            Mean time spent in state key[1], given key[0] is current state.
        """
        # Solve system of equations only for transient states.
        transient_states = sorted(self.transient_states())
        transient_matrix = self.transition_matrix[
            np.ix_(transient_states, transient_states)]
        time = self.unit_of_time * np.linalg.inv(
            np.eye(len(transient_matrix)) - transient_matrix)
        # Return as dict transforming indices to corresponding states.
        return {
            (state1, state2): time[i, j]
            for i, state1 in enumerate(transient_states)
            for j, state2 in enumerate(transient_states)
        }

    def duration_stage(self, stage: int):
        """Compute average time (days) spent in stage given we start there."""
        absorbing_states = self.absorbing_states()
        if stage in absorbing_states:
            return np.inf
        coefficients = np.eye(self.n) - self.transition_matrix
        rhs = np.zeros(self.n)
        rhs[stage] = 1
        for i in absorbing_states:
            coefficients[i, i] = 1
        result = np.linalg.solve(coefficients, rhs)
        return self.unit_of_time * result[stage]

    def duration_stages(self, stages):
        """Compute average time spent in collection of stages."""
        return sum(self.duration_stage(stage) for stage in stages)

    def simulate(self, init_stage=0):
        """Simulate until we reach absorbing state."""
        absorbing_states = self.absorbing_states()
        stage = init_stage
        history = [stage]
        cumulative = np.cumsum(self.transition_matrix, axis=1)
        while stage not in absorbing_states:
            rnd = np.random.rand()
            stage = np.argmin(cumulative[stage, :] <= rnd)
            history.append(stage)
        return history

    def hitting_probability(self, start, stages):
        """Compute probability of hitting stage(s) starting from another stage.

        Parameters
        ----------
        start: int
            Stage that you start in.
        stages: int or iterable
            Stage or stages for which we compute the hitting probability.

        Returns
        -------
        float: probability

        Note
        ----
        The probability can be computed by obtaining the minimal,
        non-negative solution to a system of equations. This requires
        special handling of the target stages and absorbing stages. After
        this, a regular linear system remains.

        """
        if isinstance(stages, int):
            stages = {stages}
        coefficients = self.transition_matrix - np.eye(self.n)
        rhs = np.zeros(self.n)
        for stage in stages:
            # Probability is 1 if we already are in one of the stages.
            coefficients[stage] = np.eye(self.n)[stage]
            rhs[stage] = 1
        for stage in self.absorbing_states():
            # If we have an absorbing state, then the probability is either 0
            # or 1, depending on whether this is a stage we are looking for.
            coefficients[stage, stage] = 1

        # Now solve the linear system and return probability for starting stage.
        hit = np.linalg.solve(coefficients, rhs)
        return hit[start]


class Transition:
    """Markov chain for COVID-19."""

    def __init__(self, markov):
        self.markov = markov

    def to_frame(self) -> pd.DataFrame:
        """Return transition matrix as DataFrame."""
        stage_index = pd.Index(STAGES.values(), name='stage')
        return pd.DataFrame(
            self.markov.transition_matrix,
            index=stage_index,
            columns=stage_index,
        )

    def summary(self) -> pd.DataFrame:
        """Compute summary statistics disease progression."""
        inn = STAGE_ID['INFECTED_NOSYMPTOMS_NOTCONTAGIOUS']
        im = STAGE_ID['INFECTED_SYMPTOMS_MILD']
        ic = STAGE_ID['INFECTED_SYMPTOMS_SEVERE_ICpossible']
        summary = {
            stage: (
                self.markov.hitting_probability(inn, i),
                self.markov.hitting_probability(im, i),
                self.markov.hitting_probability(ic, i),
                self.markov.duration_stage(i),
            )
            for i, stage in STAGES.items()
        }
        sum_df = pd.DataFrame.from_dict(
            summary,
            orient='index',
            columns=[
                'p given INN',
                'p given IM',
                'p given IC',
                'duration',
            ],
        )
        sum_df.index.set_names('stage', inplace=True)
        return sum_df


class Target(ABC):
    """Metaclass for specifying targets."""

    def __init__(self, markov: Markov, target: float, weight: float, **kwargs):
        self.markov = markov
        self.target = target
        self.weight = weight
        self.info = {}

    @abstractmethod
    def value(self) -> float:
        """Compute the value of target in Markov chain."""
        pass

    def error(self) -> float:
        """Compute difference between target and value."""
        return self.target - self.value()

    def weighted_sq_error(self):
        """Return weighted squared error for loss function."""
        return self.weight * self.error()**2

    def __str__(self):
        """Return summary of current status."""
        summary = '\n'.join([
            '-' * 60,
            'Label: {}'.format(self.info['label']),
            'Description: {}'.format(self.info['description']),
            'Age: {}'.format(self.info['age']),
            'Target: {}'.format(self.target),
            'Value: {}'.format(self.value()),
            'Weight: {}'.format(self.weight),
            'Contribution loss: {}'.format(self.weighted_sq_error()),
        ])
        return summary


class Probability(Target):
    """Probability target for Markov chain."""

    def __init__(self, *, markov, weight, start, stages, target, **kwargs):
        super().__init__(markov=markov, target=target, weight=weight)
        self.start = STAGE_ID[start]
        self.stages = stage_ids(stages)
        self.info = kwargs

    def value(self) -> float:
        """Compute the value of target in Markov chain."""
        return self.markov.hitting_probability(
            start=self.start,
            stages=self.stages,
        )


class Duration(Target):
    """Duration target for Markov chain."""

    def __init__(self, markov, *, stages, target, weight, **kwargs):
        super().__init__(markov=markov, target=target, weight=weight)
        self.stages = stage_ids(stages)
        self.info = kwargs

    def value(self) -> float:
        """Compute the value of target in Markov chain."""
        return self.markov.duration_stages(self.stages)


class TargetMaker:
    """Create targets for optimization."""

    def __init__(self, markov):
        self.markov = markov

    def read_targets(self, input_file: str, age_group=None):
        """Read targets from Excel file."""
        # Sheet name -> Target
        methods = {
            'probability': Probability,
            'duration': Duration,
        }
        targets = []
        for sheet_name, target_fun in methods.items():
            df = pd.read_excel(input_file, sheet_name=sheet_name, index_col=0)
            # Select general targets and from requested age group.
            df = df[df['age'].isin(['Age_all', age_group])]
            for _, row in df.iterrows():
                # Convert table row to dictionary, which we can use to create
                # the targets.
                kwargs = row.to_dict()
                targets.append(target_fun(markov=self.markov, **kwargs))
        return targets


class ConstraintMaker:
    """Create constraints that force valid probability matrix."""

    def __init__(self, transitions: list):
        self.transitions = transitions
        self.num_par = len(transitions)

    def get_row(self, stage):
        """Return constraint row for stage."""
        # Find indices of parameters corresponding to stage.
        indices = [self.transitions.index(entry)
                   for entry in self.transitions if entry[0] == stage]
        row = np.zeros(self.num_par, dtype=int)
        row[indices] = 1
        return row

    def get_matrix(self):
        """Return all constraint rows."""
        # Get stages for which we have at least one unknown transition.
        stages = {entry[0] for entry in self.transitions}
        return np.stack([self.get_row(stage) for stage in stages])

    def get_constraints(self):
        """Make constraints in Scipy format."""
        matrix = self.get_matrix()
        # No lower bound, upper bound one.
        lb = np.tile(-np.inf, matrix.shape[0])
        ub = np.ones(matrix.shape[0])
        return LinearConstraint(matrix, lb, ub)


class AgeModel:
    """Estimate Markov model for age group."""

    def __init__(self, excel_file: str, age: str, x0=None):
        n = len(UNKNOWN_TRANSITIONS)
        if x0 is None:
            x0 = np.random.uniform(0, 0.05, n)
        self.x = x0
        self.age = age
        self.markov = Markov(vector_to_transition_matrix(x0))
        target_maker = TargetMaker(self.markov)
        self.targets = target_maker.read_targets(excel_file, age)
        self.bounds = Bounds(np.zeros(n), np.ones(n))
        self.cons = ConstraintMaker(UNKNOWN_TRANSITIONS).get_constraints()

    def evaluate(self, x: np.ndarray = None):
        """Evaluate loss function."""
        if x is not None:
            self.set_transition_matrix(x)
        return sum(target.weighted_sq_error() for target in self.targets)

    def set_random_matrix(self):
        """Randomize transition matrix."""
        self.set_transition_matrix(
            np.random.uniform(0, 0.05, len(UNKNOWN_TRANSITIONS)))

    def set_transition_matrix(self, x: np.ndarray):
        """Update transition matrix."""
        self.x = x
        matrix = vector_to_transition_matrix(x)
        self.markov.set_transition_matrix(matrix)
        return matrix

    def optimize(self, verbose=True):
        """Optimize transition matrix."""
        options = {'disp': verbose}
        result = minimize(
            self.evaluate,
            self.x,
            bounds=self.bounds,
            constraints=self.cons,
            options=options,
        )
        self.set_transition_matrix(result.x)

    def __str__(self):
        """Return an overview of targets."""
        line = '*' * 60
        result = f'{line}\n{self.age}\n{line}\n'
        result += '\n'.join(str(target) for target in self.targets)
        result += '\n'
        return result


class MultiModel:
    """Estimate Markov models for multiple age groups."""

    max_iter = 300

    def __init__(self, models: Mapping[str, AgeModel], alpha=0.5):
        self.models = models
        self.bounds = self.merge_bounds()
        self.cons = self.merge_constraints()
        self.x = self.merge_parameters()
        self.alpha = alpha

    def __str__(self):
        """Return objective overview."""
        # result = '\n'.join(
        #     str(model) for model in self.models.values()
        # )
        result = '\n'.join([
            f'Alpha: {self.alpha}',
            f'Total loss: {self.total_loss()}',
            f'Deviation: {self.deviation()}',
            f'Weighted objective: {self.evaluate()}',
            '*' * 60,
        ])
        return result

    def set_random_parameters(self):
        """Randomize transition parameters all underlying models."""
        self.set_parameters(np.random.uniform(0, 0.05, len(self.x)))

    def set_parameters(self, x: np.ndarray):
        """Set transition parameters for all age models."""
        self.x = x
        x_as_matrix = x.reshape(len(self.models), len(UNKNOWN_TRANSITIONS))
        for xi, model in zip(x_as_matrix, self.models.values()):
            model.set_transition_matrix(xi)

    def total_loss(self):
        """Compute total loss from all models."""
        return sum(model.evaluate() for model in self.models.values())

    def evaluate(self, x: np.ndarray = None):
        """Evaluate optimization objective.

        The objective is a weighted average of the total loss and the deviation.
        """
        if x is not None:
            self.set_parameters(x)
        return (
            self.alpha * self.total_loss()
            + (1 - self.alpha) * self.deviation()
        )

    def deviation(self):
        """Return deviation measure between different age models."""
        x_per_age = self.x.reshape(len(self.models), len(UNKNOWN_TRANSITIONS))
        distance = distance_matrix(x_per_age, x_per_age)
        return distance.sum()

    def optimize_locally(self):
        """Optimize all age models independently."""
        max_iter = 4
        tol = 0.1
        for model in self.models.values():
            for _ in range(max_iter):
                model.set_random_matrix()
                model.optimize(verbose=False)
                if model.evaluate() < tol:
                    break
        self.x = self.merge_parameters()

    def optimize_globally(self):
        """Optimize the combined objective."""
        options = {'disp': True, 'maxiter': self.max_iter}
        result = minimize(
            self.evaluate,
            self.x,
            bounds=self.bounds,
            constraints=self.cons,
            options=options,
        )
        self.set_parameters(result.x)

    def optimize(self):
        """Optimize combined objective starting from local optimum."""
        self.optimize_locally()
        self.optimize_globally()

    def merge_parameters(self):
        """Read and combine parameters from individual age models.

        Concatenate all parameters in a single vector.
        """
        return np.concatenate([model.x for model in self.models.values()])

    def merge_bounds(self):
        """Return combined lower and upper bounds."""
        bounds = [model.bounds for model in self.models.values()]
        lower = np.concatenate([bound.lb for bound in bounds])
        upper = np.concatenate([bound.ub for bound in bounds])
        return Bounds(lower, upper)

    def merge_constraints(self):
        """Return combined constraints."""
        constraints = [model.cons for model in self.models.values()]
        coef_matrices = [cons.A for cons in constraints]
        coef_matrix = block_diag(*coef_matrices)
        lower = np.concatenate([cons.lb for cons in constraints])
        upper = np.concatenate([cons.ub for cons in constraints])
        return LinearConstraint(coef_matrix, lower, upper)


def get_age_groups(excel_file: str) -> list:
    """Read age groups from Excel file."""
    df = pd.read_excel(excel_file, 'age_groups', index_col=0)
    return list(df.index)


def write_results(multi_model: MultiModel, output: str, suffix: str):
    """Write transition matrices and summary statistics."""
    transitions = {
        age: Transition(model.markov)
        for age, model in multi_model.models.items()
    }
    all_transitions = pd.concat(
        {age: transition.to_frame() for age, transition in transitions.items()},
        names=['age'],
    )
    summary = pd.concat(
        {age: transition.summary() for age, transition in transitions.items()},
        names=['age'],
    )
    save_transition_matrix(all_transitions, summary, output, suffix)


def create_multi_model(in_file: str):
    """Create a MultiModel for all age groups."""
    ages = get_age_groups(in_file)
    multi_model = MultiModel({age: AgeModel(in_file, age) for age in ages})
    return multi_model


def pre_save_processing(data: pd.DataFrame):
    """Clear the HEALTHY row before writing output."""
    save_data = data.copy()
    save_data.loc[(slice(None), 'HEALTHY'), :] = 0.0
    return save_data


def save_transition_matrix(
        data: pd.DataFrame, summary: pd.DataFrame, output: str, suffix: str):
    """Save transition matrices and summary overview."""
    path = Path(f'{output}.{suffix}')
    summary_path = path.parent.joinpath(path.stem + '-summary' + path.suffix)
    save_data = pre_save_processing(data)
    if path.suffix == '.xlsx':
        save_data.to_excel(path, sheet_name='transitions', merge_cells=False)
        summary.to_excel(summary_path, merge_cells=False)
    else:
        save_data.to_csv(path)
        summary.to_csv(summary_path)


def run_multi_model(
        *,
        input_file: str,
        output: str,
        suffix: str,
        alpha: List[float],
        **kwargs,
):
    """Run MultiModel for several values of alpha."""
    multi_model = create_multi_model(input_file)
    for alp in alpha:
        multi_model.alpha = alp
        multi_model.optimize()
        print(multi_model)
        write_results(multi_model, f'{output}-{alp}', suffix)
    return multi_model


def get_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(prog='transition')
    parser.add_argument('-i', '--input_file', default='transitions-input.xlsx')
    parser.add_argument('-o', '--output', default='test')
    parser.add_argument(
        '-s', '--suffix',
        default='csv',
        choices=['csv', 'xlsx'],
    )
    parser.add_argument(
        '-a', '--alpha',
        nargs='*',
        default=[0.98, 0.99, 0.991, 0.992, 0.993, 0.994],
        type=float,
    )
    parser.add_argument('-v', '--verbose', action='store_true')
    args = parser.parse_args()
    return vars(args)


def main():
    """Run main module function."""
    args = get_args()
    multi_model = run_multi_model(**args)
    return multi_model


if __name__ == '__main__':
    model = main()
