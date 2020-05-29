const speechRecognitionConstructor = window.SpeechRecognition || window.webkitSpeechRecognition;
const textBox = document.querySelector("#textContainer");
if(speechRecognitionConstructor){
    //The browser has native support for speech recognition
    let speechRecognition = null;
    document.querySelector("#voiceRecorder").addEventListener("click", function() {
        if(!speechRecognition){
            this.classList.add("loading");
            textBox.focus();
            speechRecognition = new webkitSpeechRecognition();
            speechRecognition.onresult = (e) => {
                textBox.value = "";
                for(const result of e.results){
                    console.log(result[0].transcript);
                    if(result.isFinal){
                        textBox.value += result[0].transcript;
                        this.classList.remove("loading");
                    }
                }
            };
            speechRecognition.continuous = true;
            //Shows on intermediate results..
            speechRecognition.interimResults = true;
            speechRecognition.start();
        }else{
            speechRecognition.stop();
            speechRecognition = null;
            this.classList.remove("loading");
            textBox.blur();
        }

    });
}else{
    let recorder = null, gumStream = null;
    document.querySelector("#voiceRecorder").addEventListener("click", function() {
        if(!recorder){
            this.classList.add("loading");
            navigator.mediaDevices.getUserMedia({
                audio: true,
                video: false
            }).then(function(stream) {
                let context = new AudioContext();
                gumStream = stream;
                let input = context.createMediaStreamSource(stream);
                recorder = new Recorder(input, {
                    numberOfChannels: 1,
                    numChannels: 1
                });
                recorder.record();
            });
        }else{
            recorder.stop();
            gumStream.getAudioTracks()[0].stop();
            recorder.exportWAV(handleGeneratedAudioFile.bind(this));
        }


    });
    function handleGeneratedAudioFile(blob) {
        fetch("https://cors-anywhere.herokuapp.com/https://api.wit.ai/speech?v=20200513", {
            body: blob,
            headers: {
                "Authorization": "Bearer 3POVIIRNYZNAWPWS2J4LHHLUZXTTLGIU",
                "Content-Type": "audio/wav"
            },
            method: "POST"
        }).then(resp => resp.json())
            .then(resp => {
                textBox.value = resp.text
                this.classList.remove("loading");
            })
    }


}
