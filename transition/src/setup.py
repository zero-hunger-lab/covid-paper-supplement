"""Set up the `transition` package."""

from setuptools import setup

setup(
    name='transition',
    version='1.0',
    packages=['transition'],
    url='https://github.com/zero-hunger-lab/covid-paper-supplement',
    license='MIT',
    author='Ruud Brekelmans',
    author_email='r.c.m.brekelmans@tilburguniversity.edu',
    description='Fit virus stage transition matrix for COVID-19 simulation',
    install_requires=['numpy', 'pandas', 'scipy'],
)
