# COVID PAPER SUPPLEMENT

This repository contains the code for the COVID-19 epidemiological
simulation model as well as supporting data as used in the manuscript:

	Forecasting the spread of SARS-CoV-2 is inherently ambiguous given the
	current state of virus research

	by

	- M.F. Koenen
	- M. Balvert
	- R. Brekelmans
	- H.A. Fleuren
	- V.F. Stienen
	- J.C. Wagenaar

	Zero Hunger Lab, Tilburg University


## Source code

- The `src` folder contains the Java code of the simulation model.
- The `transition` folder contains a Python module for fitting
  transition matrices.


## Supporting data

Transition matrices and simulation initializations can be found in the
input folder in the source code.

Simulation initializations were based on a lower- and an upper bound
on the number of deaths for March 6-20. These bounds can be found in
`Deathrates_for_initialization.csv`.


## Scenario list

Scenario input files are coded according to the following table.

| ID | Symptoms | CFR            | Immunity |
|:--:|----------|----------------|----------|
|  1 |   0.375  | Data from NL   | All      |
|  2 |   0.375  | Literature-E   | All      |
|  3 |   0.375  | Literature-I-s | All      |
|  4 |   0.500  | Data from NL   | All      |
|  5 |   0.500  | Literature-E   | All      |
|  6 |   0.500  | Literature-I-s | All      |
|  7 |   0.625  | Data from NL   | All      |
|  8 |   0.625  | Literature-E   | All      |
|  9 |   0.625  | Literature-I-s | All      |
| 10 |   0.750  | Data from NL   | All      |
| 11 |   0.750  | Literature-E   | All      |
| 12 |   0.750  | Literature-I-s | All      |
| 13 |   0.375  | Data from NL   | High     |
| 14 |   0.375  | Literature-E   | High     |
| 15 |   0.375  | Literature-I-s | High     |
| 16 |   0.500  | Data from NL   | High     |
| 17 |   0.500  | Literature-E   | High     |
| 18 |   0.500  | Literature-I-s | High     |
| 19 |   0.625  | Data from NL   | High     |
| 20 |   0.625  | Literature-E   | High     |
| 21 |   0.625  | Literature-I-s | High     |
| 22 |   0.750  | Data from NL   | High     |
| 23 |   0.750  | Literature-E   | High     |
| 24 |   0.750  | Literature-I-s | High     |
| 25 |   0.375  | Data from NL   | Medium   |
| 26 |   0.375  | Literature-E   | Medium   |
| 27 |   0.375  | Literature-I-s | Medium   |
| 28 |   0.500  | Data from NL   | Medium   |
| 29 |   0.500  | Literature-E   | Medium   |
| 30 |   0.500  | Literature-I-s | Medium   |
| 31 |   0.625  | Data from NL   | Medium   |
| 32 |   0.625  | Literature-E   | Medium   |
| 33 |   0.625  | Literature-I-s | Medium   |
| 34 |   0.750  | Data from NL   | Medium   |
| 35 |   0.750  | Literature-E   | Medium   |
| 36 |   0.750  | Literature-I-s | Medium   |
| 37 |   0.375  | Data from NL   | Low      |
| 38 |   0.375  | Literature-E   | Low      |
| 39 |   0.375  | Literature-I-s | Low      |
| 40 |   0.500  | Data from NL   | Low      |
| 41 |   0.500  | Literature-E   | Low      |
| 42 |   0.500  | Literature-I-s | Low      |
| 43 |   0.625  | Data from NL   | Low      |
| 44 |   0.625  | Literature-E   | Low      |
| 45 |   0.625  | Literature-I-s | Low      |
| 46 |   0.750  | Data from NL   | Low      |
| 47 |   0.750  | Literature-E   | Low      |
| 48 |   0.750  | Literature-I-s | Low      |


### Stage conversion table

The table below shows a conversion table indicating the stage names
and abbreviations used in some of the data and source code files that
were used during development, and the stage names and abbreviations as
they are used in the final report.

| ID | short | long                                   | paper                   | paper-code |
|----|-------|----------------------------------------|-------------------------|------------|
| 0  | H     | HEALTHY                                | Susceptible             | S          |
| 1  | INN   | INFECTED_NOSYMPTOMS_NOTCONTAGIOUS      | Exposed                 | E          |
| 2  | INY   | INFECTED_NOSYMPTOMS_ISCONTAGIOUS       | Infectious asymptomatic | I-a        |
| 3  | IM    | INFECTED_SYMPTOMS_MILD                 | Infectious symptomatic  | I-s        |
| 4  | ISY   | INFECTED_SYMPTOMS_SEVERE_ICpossible    | ICU admission           | ICU-a      |
| 5  | ISN   | INFECTED_SYMPTOMS_SEVERE_ICnotpossible | ICU refusal             | ICU-r      |
| 6  | ISQ   | INFECTED_SYMPTOMS_SEVERE_QUEUE         | N/A                     | N/A        |
| 7  | C     | CURED                                  | Immune                  | IM         |
| 8  | D     | DEAD                                   | Deceased                | D          |
