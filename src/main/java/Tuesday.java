import java.util.Scanner;

/**
 * Runs the chatbot
 */
public class Tuesday {
    /**
     * Check if the input string is an available mark or unmark command
     * @param s input string
     * @param count the number of tasks in the task array
     * @return a boolean value that indicates whether the string is available
     */
    public static boolean isAvailableMark(String s, int count) {
        Scanner scMark = new Scanner(s);

        if (scMark.hasNext()) {
            String command = scMark.next();
            if (command.equals("mark") || command.equals("unmark")) {
                if (scMark.hasNextInt()) {
                    int target = scMark.nextInt();
                    return !scMark.hasNext() && target <= count && target > 0;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     *  Display the correct sentences and mark the task as done or unmark the task as not done
     * @param s the mark command for processing
     * @param tasks the tasks array's reference
     */
    public static void markProcess(String s, Task[] tasks) {
        Scanner sc = new Scanner(s);
        String command = sc.next();
        int target = sc.nextInt();
        System.out.println(Strings.horizontalLine);
        if (command.equals("mark")) {
            System.out.println(Strings.mark);
            tasks[target - 1].mark();
        } else {
            System.out.println(Strings.unmark);
            tasks[target - 1].unMark();
        }
        System.out.println("  " + tasks[target - 1].toString());
        System.out.println(Strings.horizontalLine);
    }
    /**
     * Starts the chatbot, store tasks and process the user input
     *
     * @param args
     */
    public static void main(String[] args) {
        // initialize the input, scanner and the task array with the index for input
        Scanner sc = new Scanner(System.in);
        String input = "";
        Task[] taskArray = new Task[100];
        int taskCount = 0;

        // greetings part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.banner);
        System.out.println(Strings.greeting);
        System.out.println(Strings.horizontalLine);

        // task adding part and tasks listing part
        input = sc.nextLine();
        while (! input.equals("bye")) {
            // task listing part
            if (input.equals("list")) {
                System.out.println(Strings.horizontalLine);
                System.out.println(Strings.showList);
                for (int i = 1; i < taskCount + 1; i++) {
                    System.out.println(i + "." + taskArray[i - 1].toString());
                }
                System.out.println(Strings.horizontalLine);
                input = sc.nextLine();
                continue;
            // process the mark command
            } else if (isAvailableMark(input, taskCount)) {
                markProcess(input, taskArray);
                input = sc.nextLine();
                continue;
            }
            // task adding part
            taskArray[taskCount] = new Task(input);
            taskCount++;
            System.out.println(Strings.horizontalLine);
            System.out.println("added: " + input);
            System.out.println(Strings.horizontalLine);
            input = sc.nextLine();
        }

        // farewell part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.farewell);
        System.out.println(Strings.horizontalLine);
    }
}
