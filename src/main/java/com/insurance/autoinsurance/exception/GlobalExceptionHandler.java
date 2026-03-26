package com.insurance.autoinsurance.exception;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(IllegalArgumentException e, Model model) {
        model.addAttribute("errorTitle",   "Record Not Found");
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalStateException e, Model model) {
        model.addAttribute("errorTitle",   "Invalid Operation");
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(NoResourceFoundException e, Model model) {
        model.addAttribute("errorTitle",   "Page Not Found");
        model.addAttribute("errorMessage", "The page you requested does not exist. " +
            "Check the URL or use the navigation menu.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception e, Model model) {
        model.addAttribute("errorTitle",   "Unexpected Error");
        model.addAttribute("errorMessage", "An internal error occurred. " +
            "Please try again or contact your system administrator.");
        return "error";
    }
}
