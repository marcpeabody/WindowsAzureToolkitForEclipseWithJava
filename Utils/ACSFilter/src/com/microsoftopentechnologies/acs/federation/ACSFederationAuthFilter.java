/*
 Copyright 2013 Microsoft Open Technologies, Inc. 

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
 package com.microsoftopentechnologies.acs.federation; 

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoftopentechnologies.acs.util.Utils;

public class ACSFederationAuthFilter implements Filter {
	private static final Logger LOG = Logger.getLogger(ACSFederationAuthFilter.class.getName());

	private ACSConfigurationHelper configuration;

	public void init(FilterConfig filterConfig) throws ServletException {
		Utils.logDebug("Initializing the filter..", LOG);
        configuration = new ACSConfigurationHelper(filterConfig);
	}

	public void destroy() { }

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		Utils.logDebug("In the doFilter method..", LOG);

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            ACSFilterCommand filterCommand = new ACSFilterCommand(configuration, httpRequest, httpResponse, chain);
            filterCommand.execute();
        } catch (LogonFailureException lfe) {
            sendLogOnFailureResponse(httpResponse, lfe.getMessage());
        }
	}

    private void sendLogOnFailureResponse(HttpServletResponse httpResponse, String cause) throws IOException {
        Utils.logError(cause, null, LOG);
        String errorMessage = "Provided authentication details are invalid. " + cause;
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
    }
}
