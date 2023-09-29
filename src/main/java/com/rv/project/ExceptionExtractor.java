package com.rv.project;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionExtractor {
    public static void main(String[] args) {
        String code = "@Override\n" +
                "public Void connect() throws ExecutionException {\n" +
                "    try {\n" +
                "        CommandResult result = TableauCommandRequest.executeCommand(tableauCommandRequest, TableauCommand.TABLEAU_COMMAND_DEFAULT_TIMEOUT);\n" +
                "        if (!result.isSuccessful()) {\n" +
                "            throw new ExecutionException(String.format(TABLEAU_COMMAND_S_FAILED, TableauCommand.LOGIN)).withReason(String.format(OUTPUT_FROM_TABLEAU_WAS_S_RSN, result.getOutput())).withResolution(CHECK_TABLEAU_OUTPUT_MESSAGE_RES);\n" +
                "        }\n" +
                "    } catch (InterruptedException ie) {\n" +
                "        throw new ExecutionException(ie, Messages.TABLEAU_COMMAND_FAILED_MSG).withReason(Messages.PROCESSING_INTERRUPTED_RSN).withResolution(CHECK_DATA_SIZE_RES);\n" +
                "    } catch (IOException ioe) {\n" +
                "        throw aCustomException(ioe, Messages.TABLEAU_COMMAND_FAILED_MSG).withReason(String.format(Messages.IO_ACCESS_OR_OPERATION_FAILED_RSN, ioe.getMessage())).withResolution(CHECK_SNAPLEX_RES);\n" +
                "    }\n" +
                "    return null;\n" +
                "}";

        // Define a regular expression pattern to match "throws" clauses
        Pattern pattern = Pattern.compile("throws\\s+(\\w+(\\.\\w+)?(,\\s*\\w+(\\.\\w+)?)*)");

        // Create a matcher for the code string
        Matcher matcher = pattern.matcher(code);

        // Find and print matching "throws" clauses
        while (matcher.find()) {
            String exceptions = matcher.group(1);
            String[] exceptionArray = exceptions.split(",\\s*");
            for (String exception : exceptionArray) {
                System.out.println("Thrown exception: " + exception);
            }
        }

        // Define a regular expression pattern to match "throws" clauses
        Pattern pattern1 = Pattern.compile("throw\\s+new\\s+(\\w+(\\.\\w+)?(,\\s*\\w+(\\.\\w+)?)*)");

        // Create a matcher for the code string
        Matcher matcher1 = pattern1.matcher(code);

        // Find and print matching "throws" clauses
        while (matcher1.find()) {
            String exceptions = matcher1.group(1);
            String[] exceptionArray = exceptions.split(",\\s*");
            for (String exception : exceptionArray) {
                System.out.println("throw new exception: " + exception);
            }
        }
    }
}