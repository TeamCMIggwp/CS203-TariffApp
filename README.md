# CS203-TariffApp


## Installation & Usage

1. Clone this repository

2. First time installation:
``` 
npm install
```

3. To run the file 
```
npm run dev
```

4. Some basic Git commands are:
```
git status
git add
git commit
```


## License

Credits / Authors / Acknowledgment

Contributors, Libraries, or References

# Things to note
If connecting to server, use command below so that when you close the terminal, service will not be stopped
nohup java -jar <jarFile>

If you wanna stop the service, use 
# Find the process ID (PID)
ps aux | grep java

# Or more specific for Spring Boot
ps aux | grep "your-app.jar"

# Kill the process (replace XXXX with the actual PID)
kill XXXX