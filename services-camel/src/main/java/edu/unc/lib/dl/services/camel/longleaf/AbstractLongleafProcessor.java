/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.services.camel.longleaf;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract processor for executing longleaf commands
 *
 * @author bbpennel
 */
public abstract class AbstractLongleafProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AbstractLongleafProcessor.class);
    protected static final Logger longleafLog = LoggerFactory.getLogger("longleaf");

    private String longleafBaseCommand;

    private Pattern SUCCESS_PATTERN = Pattern.compile("^SUCCESS \\w+ (.+)$");

    /**
     * Execute a command in longleaf, using the configured base command
     *
     * @param command longleaf command
     * @param pipedContent content to pipe into the command. Optional.
     * @return exit code from register command
     */
    protected ExecuteResult executeCommand(String command, String pipedContent) {
        try {
            // only register binaries with md5sum
            String longleafCommmand = longleafBaseCommand + " " + command;
            log.debug("Executing longleaf commandlongleaf: {}", longleafCommmand);

            Process process = Runtime.getRuntime().exec(longleafCommmand);
            // Pipe the manifest data into the command
            if (pipedContent != null) {
                try (OutputStream pipeOut = process.getOutputStream()) {
                    pipeOut.write(pipedContent.getBytes(UTF_8));
                }
            }

            ExecuteResult result = new ExecuteResult(process.waitFor());

            // log longleaf output
            String line;
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while ((line = in.readLine()) != null) {
                    longleafLog.info(line);
                    // If the command was not totally successful, record uris of any commands that succeed
                    if (result.failuresEncountered()) {
                        Matcher matcher = SUCCESS_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            result.completed.add(matcher.group(1));
                        }
                    }
                }
            }
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                while ((line = err.readLine()) != null) {
                    longleafLog.error(line);
                }
            }

            return result;
        } catch (IOException e) {
            log.error("IOException while executing longleaf command: {}", command, e);
        } catch (InterruptedException e) {
            log.error("InterruptedException while executing longleaf command: {}", command, e);
        }

        return new ExecuteResult(1);
    }

    protected static class ExecuteResult {
        protected int exitVal;
        protected List<String> completed = new ArrayList<>();

        protected ExecuteResult(int exitVal) {
            this.exitVal = exitVal;
        }

        protected boolean failuresEncountered() {
            return exitVal != 0;
        }
    }

    public void setLongleafBaseCommand(String longleafBaseCommand) {
        this.longleafBaseCommand = longleafBaseCommand;
    }
}
