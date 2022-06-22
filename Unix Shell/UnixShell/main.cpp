// Malila Freeman
// Date: 2/18/2022
//
// This program builds a shell from scratch that supports piping and I/O redirection. If the user types CD, it takes them to the appropriate directory.
// Additionally, this shell supports environment variables (this is my bell and whistle).

#include <iostream>
#include "shelpers.hpp"
#include <vector>
#include <string>


// This function forks for each individual command
void forkProcess(Command command){
    
    // FORK
    
    int forkPid = fork();
    
    if (forkPid == -1){
        
        perror("Error with fork\n");
        
        exit(1);
    }
    else if (forkPid == 0){
        
//        std::cout << "Child process ID: " << getpid() << "\n";
        
        std::string commandName = command.execName;
        
        
        // Only need to dupe the input if the command's output FD is anything other than std::cin (0)
        if (command.inputFd != STDIN_FILENO){
            
            // Write to the command's inputFD file descriptor instead of std::cout (1)
            int dup2Rc2 = dup2(command.inputFd, STDIN_FILENO);

            if (dup2Rc2 == -1){
                
                perror("Input file descriptor creation failed");
                
                // Close all of the open file descriptors that were opened with getCommands()
                closeOpenFDs(command);
                
                exit(1);
            }
        }
        
        // Only need to dupe the output if the command's output FD is anything other than std::cout (1)
        if (command.outputFd != STDOUT_FILENO){
            
            // Write to the command's outputFD file descriptor instead of std::cout (1)
            int dup2Rc = dup2(command.outputFd, STDOUT_FILENO);
            
            if (dup2Rc == -1){
                
                perror("Output file descriptor creation failed");
                
                // Close all of the open file descriptors that were opened with getCommands()
                closeOpenFDs(command);
                
                exit(1);
            }
        }
        
        // Call execvp to turn the shell into the appropriate command.
        // execvp takes a const char * path (which is the name of the program) and a const char * argv[] (which contains all arguments necessary
        // to execute the program)
        int execRC = execvp(const_cast<char *>(command.execName.c_str()), const_cast<char **>((command.argv.data())));
        
        if (execRC == -1){
            
            perror("Call to exec failed");
            
            exit (1);
        }
    
    }
    else {
        
//        std::cout << "Parent process ID: " << getpid() << "\n";
        
        // Close all open FDs stored in the command's openFDs vector
        if (command.openFDs.size() > 0){
            
            closeOpenFDs(command);
        }
    }
}

int main(int argc, const char * argv[]) {
    
    // Represents each string command that the user types
    std::string argumentsPassed;
    
    std::cout << "Malila's Unix Shell % ";
    
    // We want to continue getting commands until the user has quit the shell program
    while(getline(std::cin, argumentsPassed)){
        
    
        // Get a vector of commands as strings (tokens)
        std::vector <std::string> tokenizedCommands = tokenize(argumentsPassed);
        
        
        // Update the list of tokens for environment variables - vector will remain the same if there are no environment variables
        std::vector <std::string> updatedTokens = updateEnvVars(tokenizedCommands);
    
    
        // Get the list of commands: parse, set input/output file directories where necessary
        std::vector<Command> commands = getCommands(updatedTokens);
        
        
        // If the command array is empty, an error occured with getCommands() and we want to let the user know and exit out of the program 
        if (commands.size() == 0){
            
            std::cout << "Error occured getting commands.\n";
            
//            perror("Error occured getting commands");

            exit(1);
        }
        
    
        // Need to check the first command to see if it's CD or an environment varible, because these need to be handled before forking
        Command firstCommand = commands[0];
        
        std::string firstCommandName = firstCommand.execName;
        
        // Need to check to see if the first command contains an equals sign (indicating assignment of an environment variable): indexOfEquals will be -1 if it is not found
        long indexOfEquals = firstCommandName.find("=");
    
        
        // If there is no "=" in the first command, indexOfEquals will be equal to -1 and we know that the user is not trying to set an environment variable. If there is an "=" in the first command, indexOfEquals will not be equal to -1 and we know the user IS trying to set up an environment variable.
        if (indexOfEquals != -1){
            
            setEnvVar(firstCommandName, indexOfEquals);
        }
    
        // Check to see if the first command's name in the vector is cd and if it is, handle accordingly.
        else if (firstCommandName.rfind("cd", 0) == 0){
            
            handleCDCommand(firstCommand);
        }
        
        // For commands that are not establishing an environment variable or CD, we need to fork
        else {
            
            for (Command c : commands){
                
                forkProcess(c);
            }
            
            // Parent must wait for all children to complete before completing
            for (Command c : commands){
                
                int waitRC = wait(NULL);
                
                // Call to wait will return -1 if there is an error
                if (waitRC == -1){
                        
                    perror("Wait failed\n");
                    
                    // Close all open file descriptors if the wait had an error
                    closeOpenFDs(c);
                    
                    exit(1);
                }
            }
        }
        std::cout << "Malila's Unix Shell % ";
    }
    
    return 0;
}
