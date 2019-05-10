#!/usr/bin/env python

import os
from setuptools import setup
from codecs import open

root = os.path.abspath(os.path.dirname(__file__))

about = {}
with open(os.path.join(root, "pulse", "version.py"), "r", "utf-8") as f:
    exec(f.read(), about)

with open("README.md", "r", "utf-8") as f:
    readme = f.read()


packages = ["pulse"]

requires = [
    'pytz',
    'requests',
    'six',
    'future'
]

setup(
    name=about["__title__"],
    version=about["__version__"],
    url=about["__url__"],
    author=about["__author__"],
    author_email=about["__author_email__"],
    license=about["__license__"],
    packages=packages,
    install_requires=requires,
    long_description=readme,
    long_description_content_type="text/markdown",
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Natural Language :: English",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: POSIX :: Linux",
        "Programming Language :: Python :: 2",
        "Programming Language :: Python :: 2.7",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.4",
        "Programming Language :: Python :: 3.5",
        "Programming Language :: Python :: 3.6",
        "Programming Language :: Python :: 3.7",
        "Topic :: System :: Logging"
    ]
)
