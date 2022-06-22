#include "shelpers.hpp"
#include <string>


//////////////////////////////////////////////////////////////////////////////////
//
// Author: Ben Jones (I think) with a lot of clean up by J. Davison de St. Germain
//
// Date:   2019?
//         Jan 2022 - Cleanup
//
// Class: CS 6013 - Systems I
//
//////////////////////////////////////////////////////////////////////////////////

using namespace std;

////////////////////////////////////////////////////////////////////////
// Example test commands you can try once your shell is up and running:
//
// ls
// ls | nl
// cd [dir]
// cat < shelpers.cpp
// cat < shelpers.cpp | nl
// cat shelpers.cpp | nl
// cat shelpers.cpp | nl | head -50 | tail -10
// cat shelpers.cpp | nl | head -50 | tail -10 > ten_lines.txt
//
// - The following two commands are equivalent.  [data.txt is sent into nl and the
//   output is saved to numbered_data.txt.]
//
// nl > numbered_data.txt < data.txt
// nl < data.txt > numbered_data.txt
//
// - Assuming numbered_data.txt has values in it... try running:
//   [Note this probably doesn't work like one might expect...
//    does it behave the same as your normal shell?]
//
// nl < numbered_data.txt > numbered_data.txt
//
// - The following line is an error (input redirection at end of line).
//   It should fail gracefully (ie, 1) without doing anything, 2) cleaning
//   up any file descriptors that were opened, 3) giving an appropriate
//   message to the user).
//
// cat shelpers.cpp | nl | head -50 | tail -10 > ten_lines.txt < abc
//

////////////////////////////////////////////////////////////////////////
// This routine is used by tokenize().  You do not need to modify it.

bool splitOnSymbol( vector<string> & words, int i, char c )
{
   if( words[i].size() < 2 ){
      return false;
   }
   int pos;
   if( (pos = words[i].find(c)) != string::npos ){
      if( pos == 0 ){
         // Starts with symbol.
         words.insert( words.begin() + i + 1, words[i].substr(1, words[i].size() -1) );
         words[i] = words[i].substr( 0, 1 );
      }
      else {
         // Symbol in middle or end.
         words.insert( words.begin() + i + 1, string{c} );
         string after = words[i].substr( pos + 1, words[i].size() - pos - 1 );
         if( !after.empty() ){
            words.insert( words.begin() + i + 2, after );
         }
         words[i] = words[i].substr( 0, pos );
      }
      return true;
   }
   else {
      return false;
   }
}

////////////////////////////////////////////////////////////////////////
// You do not need to modify tokenize().

vector<string> tokenize( const string& s )
{
   vector<string> ret;
   int pos = 0;
   int space;

   // Split on spaces:

   while( (space = s.find(' ', pos) ) != string::npos ){
      string word = s.substr( pos, space - pos );
      if( !word.empty() ){
         ret.push_back( word );
      }
      pos = space + 1;
   }

   string lastWord = s.substr( pos, s.size() - pos );

   if( !lastWord.empty() ){
      ret.push_back( lastWord );
   }

   for( int i = 0; i < ret.size(); ++i ) {
      for( char c : {'&', '<', '>', '|'} ) {
         if( splitOnSymbol( ret, i, c ) ){
            --i;
            break;
         }
      }
   }
   return ret;
}

////////////////////////////////////////////////////////////////////////

ostream& operator<<( ostream& outs, const Command& c )
{
   outs << c.execName << " [argv: ";
   for( const auto & arg : c.argv ){
      if( arg ) {
         outs << arg << ' ';
      }
      else {
         outs << "NULL ";
      }
   }
   outs << "] -- FD, in: " << c.inputFd << ", out: " << c.outputFd << " "
        << (c.background ? "(background)" : "(foreground)");
   return outs;
}

////////////////////////////////////////////////////////////////////////
//
// getCommands()
//
// Parses a vector of command line tokens and places them into (as appropriate)
// separate Command structures.
//
// Returns an empty vector if the command line (tokens) is invalid.
//
// You'll need to fill in a few gaps in this function and add appropriate error handling
// at the end.  Note, most of the gaps contain "assert( false )".
//

vector<Command> getCommands( const vector<string> & tokens)
{
    
   vector<Command> commands( count( tokens.begin(), tokens.end(), "|") + 1 ); // 1 + num |'s commands

   int first = 0;
   int last = find( tokens.begin(), tokens.end(), "|" ) - tokens.begin();

   bool error = false;

   for( int cmdNumber = 0; cmdNumber < commands.size(); ++cmdNumber ){
      const string & token = tokens[ first ];

      if( token == "&" || token == "<" || token == ">" || token == "|" ) {
         error = true;
         break;
      }

      Command & command = commands[ cmdNumber ]; // Get reference to current Command struct.
      command.execName = token;

      // Must _copy_ the token's string (otherwise, if token goes out of scope (anywhere)
      // this pointer would become bad...) Note, this fixes a security hole in this code
      // that had been here for quite a while.

      command.argv.push_back( strdup( token.c_str() ) ); // argv0 == program name

      command.inputFd  = STDIN_FILENO;
      command.outputFd = STDOUT_FILENO;

      command.background = false;
    
      for( int j = first + 1; j < last; ++j ) {
          
         if( tokens[j] == ">" || tokens[j] == "<" ) {
             
            // Handle I/O redirection tokens
             
            // Note, that only the FIRST command can take input redirection
            // (all others get input from a pipe)
            // Only the LAST command can have output redirection
        
             // Filename represents the file that needs to do either input redirection or output redirection
             const char * filename;
             
             // The FIRST command takes INPUT redirection
             if (cmdNumber == 0 && tokens[j] == "<"){
                 
                 filename = (tokens[j++ + 1].c_str());
                 
                 // Call to open takes a const char * to the filename. The O_RDWR flag specifies that this file can be written to and read from. The
                 // O_CREAT flag specifies that the file should be created
                 // if it doesn't exist. The S_IRWXU mode specifies that the file owner has read, write, and execute permission.
                 int FD = open(filename, O_RDWR|O_CREAT, S_IRWXU);
                 
                 // Call to open will return a -1 if there is an error.
                 if (FD == -1){
                     
                     perror("Opening file descriptor failed");
                     
                     exit(1);
                 }
                 
                 // The command's input needs to come from the same FD as the file (created with open())
                 command.inputFd = FD;
                 
                 // Keep track of the command's open file descriptors so that we can close them in the parent
                 command.openFDs.push_back(command.inputFd);
                
             }
             
             // The LAST command takes OUTPUT redirection
             else if (cmdNumber == commands.size() - 1 && tokens[j] == ">"){
                 
                 filename = (tokens[j++ + 1].c_str());
                 
                 // Call to open takes a const char * to the filename. The O_WRONLY flag specifies that this file should be write-only (since
                 // we're directing the command's contents to it). The O_CREAT flag specifies that the file should be created
                 // if it doesn't exist. The S_IRWXU mode specifies that the file owner has read, write, and execute permission.
                 int FD = open(filename, O_RDWR|O_CREAT, S_IRWXU);
                 
                 // Call to open will return a -1 if there is an error
                 if (FD == -1){
                     
                     perror("Opening file descriptor failed");
                     
                     exit(1);
                 }
                 
                 // The command's output needs to write to the same FD as the file (created with open())
                 command.outputFd = FD;
                 
                 // Keep track of the command's open file descriptors so that we can close them in the parent
                 command.openFDs.push_back(command.outputFd);
                
             }
             // If the structure of the redirection tags is not as specified in the above cases, there is an error with the user's input.
             else {
                 
                 perror("Error: invalid '<' or '>' input");
                 
                 error = true;
                 
                 break;
             }
         }
         else if( tokens[j] == "&" ){
            // Fill this in if you choose to do the optional "background command" part.
            assert(false);
         }
         else {
            // Otherwise this is a normal command line argument! Add to argv.
            command.argv.push_back( tokens[j].c_str() );
         }
      }

      if( !error ) {

         if( cmdNumber > 0 ){
             
             // There are multiple commands.  Open a pipe and
             // connect the ends to the fd's for the commands!
             
             // Represents the read (or input) file descriptor of the previous command
             int previousReadFd;

             
             for (int i = 0; i < commands.size(); i++){
                 
                 // If this command is not the first command
                 if (i != 0){
                     
                     // If we are not at the first command (including the last command), we need to set the current command's file descriptor to the read file descriptor of the previous command
                     commands[i].inputFd = previousReadFd;
                     
                     // Add the command's open inputFD to its vector of open commands so that we can close
                     // it from the parent
                     commands[i].openFDs.push_back(commands[i].inputFd);
                 }
                 
                 // If this command is not the last command
                 if (i != commands.size() - 1){
                     
                     // For any commands that are not the last command, we want to create a pipe with 2 file descriptors
                     int fds[2];
                     
                     // Call to pipe takes an int array of the size of the number of file descriptors that we need
                     int pipeRC = pipe(fds);
                     
                     // Call to pipe will return a -1 if there is an error.
                     if (pipeRC == -1){
                         
                         perror("Error: pipe failed.");
                         
                         exit(1);
                     }
                     
                     // This will used by future commands so that their input will be redirected from the prior command
                     previousReadFd = fds [0];
                     
                     // Change the output file descriptors for all commands except the last one
                     commands[i].outputFd = fds[1];
                     
                     // Add the command's open outputFD to its vector of open commands so that we can close it from the parent
                     commands[i].openFDs.push_back(commands[i].outputFd);
                     
                 }
             }
         }

         // Exec wants argv to have a nullptr at the end!
         command.argv.push_back( nullptr );

         // Find the next pipe character
         first = last + 1;

         if( first < tokens.size() ){
            last = find( tokens.begin() + first, tokens.end(), "|" ) - tokens.begin();
         }
      } // end if !error
   } // end for( cmdNumber = 0 to commands.size )

   if( error ){

      // Close any file descriptors you opened in this function and return the appropriate data!

      // Note, an error can happen while parsing any command. However, the "commands" vector is
      // pre-populated with a set of "empty" commands and filled in as we go.  Because
      // of this, a "command" name can be blank (the default for a command struct that has not
      // yet been filled in).  (Note, it has not been filled in yet because the processing
      // has not gotten to it when the error (in a previous command) occurred.
    

        // Close any file descriptors that have been opened
        for (Command command : commands){
            if (command.openFDs.size() > 0){
               
                closeOpenFDs(command);
            }
        
        }
       
        // Create an empty vector to return
        vector<Command> emptyVector;
        commands = emptyVector;
           
   }

   return commands;

} // end getCommands()


///////////////////////////////////////////////////////////
// Malila's helper functions
//////////////////////////////////////////////////////////

// Takes in a command and closes all of the input and output file descriptors that have been opened for it
void closeOpenFDs (Command command){
    
    for (int openFD : command.openFDs){

        int closeRC = close(openFD);

        if (closeRC == -1){
            perror("Closing of output file descriptor failed");
            exit(1);
        }
    }
}

// FOR ENVIRONMENT VARIABLES:
// If the user has typed a command that begins in "$", we know that it refers to an environment variable. Therefore, we want to change the name of the command string in the vector of tokens using "getenv" to be the name that the environment variable refers to.
std::vector <std::string> updateEnvVars (std::vector <std::string> tokenizedCommands){
    
    std::vector <std::string> commandsToReturn = tokenizedCommands;
    
    for (int i = 0; i < tokenizedCommands.size(); i++){
        
        // Check to see if there's a token that begins with "$", which indicates an environement variable
        if (tokenizedCommands[i].rfind("$", 0) == 0){
            
            // This will parse the name of the variable, which we will use for getenv
            std::string nameOfEnvVar = tokenizedCommands[i].substr(1, tokenizedCommands[i].length());
            
            // Make the getenv call, which takes in the environment variable name as a parameter
            char * getenvRC = getenv(nameOfEnvVar.c_str());
            
            if (getenvRC == NULL){
                
                perror("Environment variable not recognized");
                
                exit(1);
            }
            else {
                
                // Update the tokens vector with the corresponding value string
                std::string envVar(getenvRC);
                
                commandsToReturn[i] = envVar;
            }
        }
    }
    
    return commandsToReturn;
}

// FOR ENVIRONMENT VARIABLES:
// This sets a specified environment variable name to the specified value if the first command that the user types in includes "="
void setEnvVar (std::string firstCommandName, long indexOfEquals){
    
    // The name of the environment variable to be set will come before the "="
    std::string nameOfEnvVar = firstCommandName.substr(0, indexOfEquals);

    // The name of the value for the environment variable to be assigned to will come after the "="
    std::string valOfEnvVar = firstCommandName.substr((indexOfEquals + 1), firstCommandName.length());

    // If overwrite is nonzero, setenv will change the value associated with the variable name if it already exists in the environment.
    // Basically, this allows for updating of variable names.
    int overwrite = 1;

    int setenvRC = setenv(nameOfEnvVar.c_str(), valOfEnvVar.c_str(), overwrite);

    if (setenvRC == -1){
        
        perror("Issue with setting environment variable.");
        
        exit(1);
    }
}

// FOR CD:
// If the user has typed cd with no other arguments, shell goes to the user's home directory. If the user has typed cd with one other argument (the specified directory), the shell will go to this directory.
void handleCDCommand (Command firstCommand){
    
    // Need to adjust the file path according to whether there is a parameter or not
    std::string filePath = "";
    
    
    // CD with no parameter: if only CD is provided, go to the $HOME directory
    // We know only CD is provided if the size == 2 because there should be "cd" and NULL in argv
    if (firstCommand.argv.size() == 2){
        
        filePath = getenv("HOME");
    }
    
    // CD with a parameter: if a parameter is provided, go to the directory provided
    else {
        
        filePath = firstCommand.argv[1];
        
    }
    
    // Call to chdir, which takes a const char * to the path (either $HOME or the directory specified in the parameter)
    int cdRC = chdir((filePath.c_str()));
    
    // Call to chdir will return -1 if there is an error
    if (cdRC == -1){
        
        perror("Error with changing directory");
        
        exit(1);
    }
}


