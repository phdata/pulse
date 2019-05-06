from distutils.core import setup

setup (
    name='pulse-logging',
    version='2.0',
    packages=["pulse"],
    install_requires=['requests', 'pytz', 'six', 'future'],
    long_description=open('README.md').read()
)
