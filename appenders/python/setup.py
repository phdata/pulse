from distutils.core import setup

setup (
    name='pulse',
    version='2.0',
    packages=["pulse_appenders"],
    install_requires=['requests', 'pytz', 'six'],
    long_description=open('README.md').read()
)
