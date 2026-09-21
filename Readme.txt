1. SSH into a dc machine:
   
   ssh exp240006@dc01.utdallas.edu

2. Navigate to project directory:
   
   cd /home/012/e/ex/exp240006/project1

3. Compile the Java file:
   
   javac Node.java

   chmod +x launcher.sh cleanup.sh


    ./launcher.sh config.txt
   
   Replace "config.txt" with your actual config file name.

   When done or to stop all running nodes:

   ./cleanup.sh config.txt

Replace "config.txt" with the same config file you used to launch.