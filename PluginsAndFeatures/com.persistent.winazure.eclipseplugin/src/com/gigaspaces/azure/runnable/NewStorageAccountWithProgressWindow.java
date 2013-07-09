/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.gigaspaces.azure.runnable;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;

import waeclipseplugin.Activator;
import com.gigaspaces.azure.model.CreateStorageServiceInput;
import com.gigaspaces.azure.model.StorageService;
import com.gigaspaces.azure.rest.RestAPIConflictException;
import com.gigaspaces.azure.rest.RestAPIException;
import com.gigaspaces.azure.tasks.AccountCachingExceptionEvent;
import com.gigaspaces.azure.util.CommandLineException;
import com.gigaspaces.azure.util.PublishData;
import com.gigaspaces.azure.wizards.Messages;
import com.gigaspaces.azure.wizards.WizardCacheManager;
import com.microsoftopentechnologies.wacommon.utils.WACommonException;
import com.persistent.util.MessageUtil;

public class NewStorageAccountWithProgressWindow
extends AccountActionRunnable implements Runnable {


	private CreateStorageServiceInput body;
	static StorageService storageService;
	private final static int TASKS = 200;

	public NewStorageAccountWithProgressWindow(PublishData data, Shell shell) {
		super(data, shell);
	}

	public void setCreateStorageAccount(CreateStorageServiceInput body) {
		this.body = body;
	}

	@Override
	public void run() {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		try {
			dialog.run(true, true, this);
			dialog.close();
		} catch (InvocationTargetException e) {
			MessageUtil.displayErrorDialog(shell,
					com.gigaspaces.azure.wizards.Messages.createStorageAccountFailedTitle,
					e.getMessage());
			Activator.getDefault().log(Messages.error, e);
		} catch (InterruptedException e) {
			MessageDialog.openWarning(shell,
					Messages.interrupt,
					Messages.newStorageInterrupted);
			Activator.getDefault().log(Messages.error, e);
		}
	}

	@Override
	public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {

		monitor.beginTask(Messages.crtStrgAcc
				+ body.getServiceName()
				+ Messages.takeMinLbl,
				TASKS);

		Thread thread = doAsync();

		while (wait.get()) {
			if (monitor.isCanceled()) {
				thread.interrupt();
				throw new InterruptedException();
			}
			Thread.sleep(1000);
			monitor.worked(1);
		}
		if (error.get()) {
			monitor.worked(TASKS);
			monitor.done();
			throw new InvocationTargetException(exception, errorMessage);
		}
		monitor.worked(TASKS);
		monitor.done();
		thread.join();
	}

	public static StorageService getStorageService() {
		return storageService;
	}

	@Override
	public void doTask() {
		try {
			storageService = WizardCacheManager.createStorageAccount(body);
		}
		catch (RestAPIConflictException e) {
			AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
			event.setException(e);
			event.setMessage(Messages.storageAccountConflictError);
			onRestAPIError(event);
			Activator.getDefault().log(Messages.error, e);
		}
		catch (RestAPIException e) {
			AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
			event.setException(e);
			event.setMessage(e.getMessage());
			onRestAPIError(event);
			Activator.getDefault().log(Messages.error, e);
		}
		catch (InterruptedException e) {
		}
		catch (CommandLineException e) {
			AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
			event.setException(e);
			event.setMessage(e.getMessage());
			onRestAPIError(event);
			Activator.getDefault().log(Messages.error, e);
		}
		catch(WACommonException e) {
			Activator.getDefault().log(Messages.error, e);
			e.printStackTrace();
		}
	}
}
