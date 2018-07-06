from distutils.core import setup

version =

setup(
    name='pythonAppender',
    version='1.0',
    packages=['pythonAppender'],
    install_requires=['logging', 'requests'],
    long_description=open('README.txt').read()
)