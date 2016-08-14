package ch.bergturbenthal.coreos.manager.controller;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ConfigController {
	@Autowired
	private ConfigurationService configurationService;

	@GetMapping(path = "config/**")
	public Resource config(final HttpServletRequest request, @RequestParam("mac") final String mac) throws IOException {
		try {
			final String filename = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
			final String onlyFilename = filename.substring(8);
			log.info("Loading config " + onlyFilename + " for " + mac);
			return configurationService.generateFile(onlyFilename, mac);
		} catch (final FileNotFoundException ex) {
			throw new ResourceNotFoundException("config not found", ex);
		}
	}

	@ExceptionHandler(Exception.class)
	public ModelAndView handleError(final HttpServletRequest req, final Exception exception) {
		log.error("Request: " + req.getRequestURL() + " raised ", exception);

		final ModelAndView mav = new ModelAndView();
		mav.addObject("exception", exception);
		mav.addObject("url", req.getRequestURL());
		mav.addObject("stringUtils", StringUtils.class);
		mav.setViewName("error-detail");
		return mav;
	}
}
