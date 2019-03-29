# Python custom log Handler
Installing appender by running the setup.py script

```
python setup.py install
```

Then you can import the package

```
from pulse_appender import RequestsHandler
```

and follow the example in the `pulse_appender` directory.


RequestsHandler takes 3 Arguments hostname, path, Method (currently just post method is implemented). 
The LogFormatter converts log record to json format so they can be posted to the Pulse log collector endpoint.

