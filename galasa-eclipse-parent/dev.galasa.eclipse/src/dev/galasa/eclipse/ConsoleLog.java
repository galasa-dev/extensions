/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse;

import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class ConsoleLog implements Log {

    private MessageConsole console;
    private PrintStream    consoleDefault;
    private PrintStream    consoleRed;
    private PrintStream    consoleBlue;

    public ConsoleLog() {
        // Look for existing console
        ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
        IConsoleManager consoleManager = consolePlugin.getConsoleManager();
        IConsole[] existingConsoles = consoleManager.getConsoles();
        for (IConsole existingConsole : existingConsoles) {
            if (existingConsole.getName().equals(Activator.PLUGIN_NAME)) {
                console = (MessageConsole) existingConsole;
                break;
            }
        }

        // Not found, create a new one
        if (console == null) {
            console = new MessageConsole(Activator.PLUGIN_NAME, null);
            consoleManager.addConsoles(new IConsole[] { console });
        }

        // activate console
        console.activate();

        // Create the default PrintStream
        MessageConsoleStream messageConsoleStreamDefault = console.newMessageStream();
        messageConsoleStreamDefault.setColor(null);
        consoleDefault = new PrintStream(messageConsoleStreamDefault, true);

        // Create a PrintStream for Red text
        MessageConsoleStream messageConsoleStreamRed = console.newMessageStream();
        messageConsoleStreamRed.setColor(new Color(null, new RGB(255, 0, 0)));
        consoleRed = new PrintStream(messageConsoleStreamRed, true);

        // Create a PrintStream for Blue text
        MessageConsoleStream messageConsoleStreamBlue = console.newMessageStream();
        messageConsoleStreamBlue.setColor(new Color(null, new RGB(0, 0, 255)));
        consoleBlue = new PrintStream(messageConsoleStreamBlue, true);
    }

    public void writeMessage(PrintStream stream, Object message, Throwable t) {
        stream.println(message);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    @Override
    public void trace(Object message) {
        if (isTraceEnabled()) {
            writeMessage(consoleDefault, message, null);
        }
    }

    @Override
    public void trace(Object message, Throwable t) {
        if (isTraceEnabled()) {
            writeMessage(consoleDefault, message, null);
        }
    }

    @Override
    public void debug(Object message) {
        if (isDebugEnabled()) {
            writeMessage(consoleDefault, message, null);
        }
    }

    @Override
    public void debug(Object message, Throwable t) {
        if (isDebugEnabled()) {
            writeMessage(consoleDefault, message, null);
        }
    }

    @Override
    public void info(Object message) {
        if (isInfoEnabled()) {
            writeMessage(consoleDefault, message, null);
        }
    }

    @Override
    public void info(Object message, Throwable t) {
        if (isInfoEnabled()) {
            writeMessage(consoleDefault, message, null);
        }
    }

    @Override
    public void warn(Object message) {
        if (isWarnEnabled()) {
            writeMessage(consoleBlue, message, null);
        }
    }

    @Override
    public void warn(Object message, Throwable t) {
        if (isWarnEnabled()) {
            writeMessage(consoleBlue, message, null);
        }
    }

    @Override
    public void error(Object message) {
        if (isErrorEnabled()) {
            writeMessage(consoleRed, message, null);
        }
    }

    @Override
    public void error(Object message, Throwable t) {
        if (isErrorEnabled()) {
            writeMessage(consoleRed, message, null);
        }
    }

    @Override
    public void fatal(Object message) {
        if (isFatalEnabled()) {
            writeMessage(consoleRed, message, null);
        }
    }

    @Override
    public void fatal(Object message, Throwable t) {
        if (isFatalEnabled()) {
            writeMessage(consoleRed, message, null);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isFatalEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

}
