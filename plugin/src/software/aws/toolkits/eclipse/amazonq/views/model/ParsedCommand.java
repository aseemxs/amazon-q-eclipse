// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class ParsedCommand {

    private final Command command;
    private final Object params;
    private final String requestId;

    public ParsedCommand(final Command command, final Object params, final String requestId) {
        this.command = command;
        this.params = params;
        this.requestId = requestId;
    }

    public final Command getCommand() {
        return this.command;
    }

    public final Object getParams() {
        return this.params;
    }

    public final String getRequestId() {
        return this.requestId;
    }

}
