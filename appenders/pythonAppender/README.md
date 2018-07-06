#Python custom log Handler
- Install pythonAppender
`pip install appenders/pythonAppender`
- Import RequestsHandler to your program
`import RequestsHandler`
- RequestsHandler takes 3 Arguments hostname, url, Method (currently just post method is implemented)
- LogFormatter converts log record to json format
- RequestsHandler is set to use LogFormatter
