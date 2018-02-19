#!/ bin/sh


echo " Waiting ␣ for␣ registry ␣to␣ start ..."
sleep 5
java -Djava.rmi.server.codebase = http://users.ecs.soton.ac.uk/tjn1f15/comp2207.jar MyServer &
echo " Waiting ␣ for␣ remote ␣ server ␣to␣ start ..."
sleep 5
java MyClient localhost tjn1f15 &
java MyClient localhost tjn1f15 &
java MyClient localhost tjn1f15
