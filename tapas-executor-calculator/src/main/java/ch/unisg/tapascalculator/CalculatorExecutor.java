package ch.unisg.tapascalculator;

import ch.unisg.tapascalculator.domain.Task;
import ch.unisg.tapascalculator.services.ExecutionUseCase;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CalculatorExecutor implements ExecutionUseCase {

    @Override
    public CompletableFuture<Task> executeTask(final Task task) {
        return CompletableFuture.supplyAsync(() -> {

            // Validate task input
            if (task == null || task.getInput() == null || task.getInput().trim().isEmpty()) {
                throw new IllegalArgumentException("Task or task input cannot be null or empty");
            }
            try {
                // Improved regex to split input
                String[] inputToExecutor = task.getInput().split("\\s*([+*/\\-^%])\\s*");
                if (inputToExecutor.length != 2) {
                    throw new IllegalArgumentException("Invalid input format");
                }

                // Parsing numbers
                int num1 = Integer.parseInt(inputToExecutor[0]);
                int num2 = Integer.parseInt(inputToExecutor[1]);

                Pattern pattern = Pattern.compile("\\s*([+*/\\-^%])\\s*");
                Matcher matcher = pattern.matcher(task.getInput());
                String opString = "";
                if (matcher.find()) opString = matcher.group(1);

                // Output variable
                String out;

                // Perform the operation
                switch (opString) {
                    case "+" -> out = String.valueOf(num1 + num2);
                    case "-" -> out = String.valueOf(num1 - num2);
                    case "*" -> out = String.valueOf(num1 * num2);
                    case "/" -> {
                        if (num2 == 0) {
                            throw new ArithmeticException("Division by zero");
                        }
                        out = String.valueOf(num1 / num2);
                    }
                    default -> throw new UnsupportedOperationException("Unsupported operation: " + opString);
                }

                // Set output for task
                task.setOutput(out);

            } catch (NumberFormatException e) {
                task.setOutput("Invalid number format");
            } catch (ArithmeticException e) {
                task.setOutput("Error: " + e.getMessage());
            } catch (Exception e) {
                task.setOutput("An error occurred: " + e.getMessage());
            }

            return task;
        });
    }
}


/* Hystrix Circuit Breaker Implementation:

package ch.unisg.tapascalculator;

import ch.unisg.tapascalculator.domain.Task;
import ch.unisg.tapascalculator.services.ExecutionUseCase;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CalculatorExecutor implements ExecutionUseCase {

    @Override
    public Task executeTask(final Task task) {
        try {
            return new ExecuteTaskCommand(task).execute();
        } catch (Exception e) {
            System.out.println("Task execution failed at Executor");
            task.setOutput("Error: Task execution failed at Executor");
            return task;
        }
    }

    private static class ExecuteTaskCommand extends HystrixCommand<Task> {
        private final Task task;

        protected ExecuteTaskCommand(Task task) {
            super(HystrixCommandGroupKey.Factory.asKey("CalculatorExecutor"));
            this.task = task;
        }

        @Override
        protected Task run() throws Exception {
            // get input from task input data
            String[] inputToExecutor = task.getInput().split("([*+/-])");

            // get number1, number 2 from task input data
            int num1 = Integer.parseInt(inputToExecutor[0].trim());
            int num2 = Integer.parseInt(inputToExecutor[1].trim());

            Pattern pattern = Pattern.compile("([*+/-])");
            Matcher matcher = pattern.matcher(task.getInput());
            if (!matcher.find()) {
                throw new IllegalArgumentException("Invalid operation");
            }
            String opString = matcher.group(1);

            // calculator: add numbers
            String out;
            switch (opString) {
                case "+" -> out = String.valueOf(num1 + num2);
                case "-" -> out = String.valueOf(num1 - num2);
                case "*" -> out = String.valueOf(num1 * num2);
                case "/" -> out = num2 != 0 ? String.valueOf(num1 / num2) : "Error: Division by zero";
                default -> throw new IllegalArgumentException("Invalid operation");
            }

            // set output for task
            task.setOutput(out);
            return task;
        }

        /* @Override */
        /*
        protected Task getFallback() {
            task.setOutput("Error: Task execution failed at Executor");
            return task;
        }
    }
}

*/