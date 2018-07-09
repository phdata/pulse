#Python custom log Handler
- Install pulse_appender
`pip install [project_root]/appenders/python/pulse_appender`
- Import RequestsHandler to your program
`import RequestsHandler`
- RequestsHandler takes 3 Arguments hostname, url, Method (currently just post method is implemented)
- LogFormatter converts log record to json format
- RequestsHandler is set to use LogFormatter
