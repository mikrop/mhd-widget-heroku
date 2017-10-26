package cz.mikropsoft.mhdwidget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.ExceptionMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    /**
     * Catch all for any other exceptions...
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAnyException(Exception e) {
        return errorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

//    /**
//     * Handle failures commonly thrown from code
//     */
//    @ExceptionHandler({ InvocationTargetException.class, IllegalArgumentException.class, ClassCastException.class,
//            ConversionFailedException.class })
//    @ResponseBody
//    public ResponseEntity handleMiscFailures(Throwable t) {
//        return errorResponse(t, HttpStatus.BAD_REQUEST);
//    }
//
//    /**
//     * Send a 409 Conflict in case of concurrent modification
//     */
//    @ExceptionHandler({ ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class,
//            DataIntegrityViolationException.class })
//    @ResponseBody
//    public ResponseEntity handleConflict(Exception ex) {
//        return errorResponse(ex, HttpStatus.CONFLICT);
//    }

    protected ResponseEntity<ExceptionMessage> errorResponse(Throwable throwable, HttpStatus status) {
        if (null != throwable) {
            log.error("error caught: " + throwable.getMessage(), throwable);
            return response(new ExceptionMessage(throwable), status);
        } else {
            log.error("unknown error caught in RESTController, {}", status);
            return response(null, status);
        }
    }

    protected <T> ResponseEntity<T> response(T body, HttpStatus status) {
        log.debug("Responding with a status of {}", status);
        return new ResponseEntity<>(body, new HttpHeaders(), status);
    }

}
