// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public record ChatOptions(QuickActions quickActions, boolean history, boolean export, boolean mcpServers) { }
