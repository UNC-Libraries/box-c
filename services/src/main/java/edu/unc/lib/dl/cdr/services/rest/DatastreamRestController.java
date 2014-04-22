package edu.unc.lib.dl.cdr.services.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.ui.service.FedoraContentService;
import edu.unc.lib.dl.util.ContentModelHelper;

@Controller
public class DatastreamRestController {
	@Autowired
	private FedoraContentService fedoraContentService;

	@RequestMapping("/file/{pid}")
	public void getDatastream(@PathVariable("pid") String pid,
			@RequestParam(value = "dl", defaultValue = "false") boolean download,
			@CookieValue(value = "_ga", required = false) String gaCid,
			HttpServletRequest request, HttpServletResponse response) {
		fedoraContentService.streamData(pid, null, download, gaCid, response);
	}

	@RequestMapping("/file/{pid}/{datastream}")
	public void getDatastream(@PathVariable("pid") String pid, @PathVariable("datastream") String datastream,
			@RequestParam(value = "dl", defaultValue = "false") boolean download,
			@CookieValue(value = "_ga", required = false) String gaCid,
			HttpServletRequest request, HttpServletResponse response) {
		fedoraContentService.streamData(pid, datastream, download, gaCid, response);
	}

	@RequestMapping("/thumb/{pid}")
	public void getThumbnailSmall(@PathVariable("pid") String pid,
			@RequestParam(value = "size", defaultValue = "small") String size, HttpServletRequest request,
			HttpServletResponse response) {
		fedoraContentService.streamData(pid, ContentModelHelper.Datastream.THUMB_SMALL.getName(), false, null, response);
	}

	@RequestMapping("/thumb/{pid}/{size}")
	public void getThumbnail(@PathVariable("pid") String pid,
			@PathVariable("size") String size, HttpServletRequest request,
			HttpServletResponse response) {
		String datastream = ("large".equals(size)) ? ContentModelHelper.Datastream.THUMB_LARGE.getName()
				: ContentModelHelper.Datastream.THUMB_SMALL.getName();
		fedoraContentService.streamData(pid, datastream, false, null, response);
	}
}