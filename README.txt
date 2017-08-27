This is a file developed by me to detect the snoring of a person. I want to mention some very important points:

1.  I have implemented FFT operation and Complex variable ibraries here. Although, many others are also available, I have developed
    this for myself as a practice. I have not recursion in implementation, I have first calculated the twiddle multipliers for each
    stage of N point FFT. I have assumed that the N is power of 2. Then for calculaing bitreversed indices, I have used recursion,
    and stored the indices in an integer array. All of this to speed up the process of calculating FFT. Once I have the indices then
    I do not need to worry rearranging the input {Sorry, forgot to mention I am using Decimation in Time FFT}. Only things that need
    to be done while calculating the FFT are complex multiplication and additions.
    
2.  I have simply used the raw data obtained from the microphone. The tehnique to detect snoring is similar as given in following
    IEEE paper: "http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7056317&tag=1". Here I have set lower and higher frequencies
    area as they have mentioned. But I have not low pass filtered the raw data, due to which I am getting a lot of noise. A simple
    averaging filter, with moving window as mentioned in the paper is very much effective in doing such work.
    
3.  If you want to use the FFT technique as in this app, you are free to use, but on one condition. Let me know what all are the 
    shortcomings, what can be done to improve it. Also let me know if there are bugs in the algorithm :) ALSO, SINCE ASYNC TASKS 
    ARE EXECUTING THE FFT PARALLELY, THE OUTPUT THAT THE PERSON IS SNORING IS DELAYED.
    
4.  In app, there are 2 buttons. Start will start recording and start plotting Amplitude by default. But once the second button is
    activated, you can change between the Frequency and Amplitude plot. The plotting is done by GraphView library. Not because it
    is better than others (I am not saying that it is not), but because I am quite familier with it and it's easy to use.
    


SIDDHARTH LAKHERA
ATDC,IIT Kharagpur
