Tanushri Singh - tts150030
Ko-Chen Chen - kxc170002
Andrew Shirley - ars092220

Make sure you have set up keys for passwordless login to dc machines
Make sure you have these 3 directories on dc machines
CS6349/Project/merchant/
CS6349/Project/broker/
CS6349/Project/client/

If you don't have konsole installed, in all launcher.sh and cleanup.sh change konsole to your preferred command line application

To start up merchants
1. Change netid to your netid in merchant/launcher.sh and merchant/cleanup.sh
2. Launch launcher.sh
3. Wait for the files to copy over to the dc machines
4. Close the next window that popped up (this is the javac *.java window)
5. 2 merchants should have started

To start up broker
1. Change netid to your netid in broker/launcher.sh
2. Launch launcher.sh
3. Wait for the files to copy over to the dc machines
4. Close the next window that popped up (this is the javac *.java window)
5. In the next popup navigate to, cd CS6349/Project/broker/src/
6. Then run, java NSBroker

To start up client
1. Change netid to your netid in client/launcher.sh
2. Launch launcher.sh
3. Wait for the files to copy over to the dc machines
4. Close the next window that popped up (this is the javac *.java window)
5. In the next popup navigate to, cd CS6349/Project/client/src/
6. Then run, java Main

To Stop everything
1. Ctrl-C in the window you used to start client/launcher.sh
2. Ctrl-C in the window you used to start broker/launcher.sh
3. Launch merchant/cleanup.sh