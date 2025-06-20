// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.configuration.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

public final class SignoutAction extends Action {

    public SignoutAction() {
        setText("Sign out");
    }

    @Override
    public void run() {
        UiTelemetryProvider.emitClickEventMetric("auth_signOut");
        ThreadingUtils.executeAsyncTask(() -> {
            try {
                AuthState authState = Activator.getLoginService().getAuthState();
                if (!authState.isLoggedOut()) {
                    Activator.getLoginService().logout().get();
                }
            } catch (Exception e) {
                PluginUtils.showErrorDialog("Amazon Q", "An error occurred while attempting to sign out of Amazon Q. Please try again.");
                Activator.getLogger().error("Failed to sign out", e);
                return;
            }

            Activator.getLogger().info("Signed out of Amazon Q");
            Activator.getPluginStore().remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
            ThreadingUtils.executeAsyncTask(() -> CustomizationUtil.triggerChangeConfigurationNotification());
        });
    }

    public void setVisible(final boolean isVisible) {
        super.setEnabled(isVisible);
    }

}
