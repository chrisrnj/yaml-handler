package com.epicnicity322.yamlhandler.exceptions;

public class InvalidConfigurationException extends Exception
{
    public InvalidConfigurationException()
    {
    }

    public InvalidConfigurationException(String msg)
    {
        super(msg);
    }

    public InvalidConfigurationException(Throwable cause)
    {
        super(cause);
    }

    public InvalidConfigurationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
