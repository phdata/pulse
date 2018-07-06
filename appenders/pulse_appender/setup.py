from distutils.core import setup

setup (
    name='pulse_appender',
    version='1.0',
    py_modules=['LogFormatter','RequestsHandler'],
    install_requires=['requests'],
    long_description=open('README.md').read()
)
