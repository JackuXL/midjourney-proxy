package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskQueueHelper;
import com.github.novicezk.midjourney.util.MimeTypeUtils;
import eu.maxschuster.dataurl.DataUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
	private final TaskStoreService taskStoreService;
	private final DiscordService discordService;
	private final TaskQueueHelper taskQueueHelper;

	@Override
	public SubmitResultVO submitImagine(Task task, List<DataUrl> dataUrls) {
		return this.taskQueueHelper.submitTask(task, () -> {
			List<String> imageUrls = new ArrayList<>();
			for (DataUrl dataUrl : dataUrls) {
				String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
				Message<String> uploadResult = this.discordService.upload(taskFileName, dataUrl);
				if (uploadResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(uploadResult.getCode(), uploadResult.getDescription());
				}
				String finalFileName = uploadResult.getResult();
				Message<String> sendImageResult = this.discordService.sendImageMessage("upload image: " + finalFileName, finalFileName);
				if (sendImageResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(sendImageResult.getCode(), sendImageResult.getDescription());
				}
				imageUrls.add(sendImageResult.getResult());
			}
			if (!imageUrls.isEmpty()) {
				task.setPrompt(String.join(" ", imageUrls) + " " + task.getPrompt());
				task.setPromptEn(String.join(" ", imageUrls) + " " + task.getPromptEn());
				task.setDescription("/imagine " + task.getPrompt());
				this.taskStoreService.save(task);
			}
			return this.discordService.imagine(task.getPromptEn(), task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE));
		});
	}

	@Override
	public SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags) {
		return this.taskQueueHelper.submitTask(task, () -> this.discordService.upscale(targetMessageId, index, targetMessageHash, messageFlags, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE)));
	}

	@Override
	public SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags) {
		return this.taskQueueHelper.submitTask(task, () -> this.discordService.variation(targetMessageId, index, targetMessageHash, messageFlags, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE)));
	}

	@Override
	public SubmitResultVO submitReroll(Task task, String targetMessageId, String targetMessageHash, int messageFlags) {
		return this.taskQueueHelper.submitTask(task, () -> this.discordService.reroll(targetMessageId, targetMessageHash, messageFlags, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE)));
	}

	@Override
	public SubmitResultVO submitDescribe(Task task, DataUrl dataUrl) {
		return this.taskQueueHelper.submitTask(task, () -> {
			String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
			Message<String> uploadResult = this.discordService.upload(taskFileName, dataUrl);
			if (uploadResult.getCode() != ReturnCode.SUCCESS) {
				return Message.of(uploadResult.getCode(), uploadResult.getDescription());
			}
			String finalFileName = uploadResult.getResult();
			return this.discordService.describe(finalFileName, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE));
		});
	}

	@Override
	public SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions) {
		return this.taskQueueHelper.submitTask(task, () -> {
			List<String> finalFileNames = new ArrayList<>();
			for (DataUrl dataUrl : dataUrls) {
				String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
				Message<String> uploadResult = this.discordService.upload(taskFileName, dataUrl);
				if (uploadResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(uploadResult.getCode(), uploadResult.getDescription());
				}
				finalFileNames.add(uploadResult.getResult());
			}
			return this.discordService.blend(finalFileNames, dimensions, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE));
		});
	}

	@Override
	public SubmitResultVO submitZoom(Task task, String targetMessageId, String targetMessageHash, int messageFlags) {
		return this.taskQueueHelper.submitTask(task, () -> {
			String zoomOut;
			if(task.getAction().equals(TaskAction.ZOOM_1)){
				zoomOut="75";
			} else if (task.getAction().equals(TaskAction.ZOOM_2)) {
				zoomOut = "50";
			}else {
				zoomOut = "50";
			}

			return this.discordService.zoom(targetMessageId, targetMessageHash, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE),zoomOut);
		});
	}

	@Override
	public SubmitResultVO submitVary(Task task, String targetMessageId, String targetMessageHash, int messageFlags) {
		return this.taskQueueHelper.submitTask(task, () -> {
			String vary;
			if (task.getAction().equals(TaskAction.VARY_HIGH)) {
				vary = "high_variation";
			} else {
				vary = "low_variation";
			}
			return this.discordService.vary(targetMessageId, targetMessageHash, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE), vary);
		});
	}

	@Override
	public SubmitResultVO move(Task task, String targetMessageId, String targetMessageHash, int messageFlags) {
		return this.taskQueueHelper.submitTask(task, () -> {
			String move = "pan_up";
			if (task.getAction().equals(TaskAction.MOVE_UP)) {
				move = "pan_up";
			} else if (task.getAction().equals(TaskAction.MOVE_DOWN)) {
				move = "pan_down";
			}if (task.getAction().equals(TaskAction.MOVE_LEFT)) {
				move = "pan_left";
			}if (task.getAction().equals(TaskAction.MOVE_RIGHT)) {
				move = "pan_right";
			}
			return this.discordService.move(targetMessageId, targetMessageHash, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE), move);
		});
	}
}
