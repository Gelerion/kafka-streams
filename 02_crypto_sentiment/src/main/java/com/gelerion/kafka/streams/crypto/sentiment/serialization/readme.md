### Handling Deserialization Errors in Kafka Streams
You should always be explicit with how your application handles deserialization errors, otherwise you may get an 
unwelcome surprise if a malformed record ends up in your source topic

Kafka Streams has a special config, `DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG`,   
that can be used to specify a deserialization exception handler.

You can implement your own exception handler, or use one of the built-in defaults, 
including `LogAndFailExceptionHandler` (which logs an error and then sends a shutdown signal to Kafka Streams) or 
`LogAndContinueExceptionHandler` (which logs an error and continues processing