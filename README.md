# LogParser
Log Analyzer Project Rough Feature List:

Basic Features:
- GUI
- The capability to split logs containing multiple connections into individual logs.
- A search tool
- A way to convert hex protocol to wireshark trace (see ChrisM's app at C:\libs\support\apps\ToHexdump) or plain text. 

Intermidiate Features:
- A way to split out multiple threads into a sequential log (HTTP Parallel Paging)
- Deobfuscate stack traces (This can be lowest priority. If you have better ideas for features, they may be worth adding over this)

Advanced Features:
- Convert logfiles to HTTPTestFiles

Considerations:
- Performance. Memory and Time. The analyzer should stream data for large files/parse optimally.
- Design. The analyzer should be intuitive and easy to use
- Reliablity. As this is working on customer data the analyzer should handle all manner of logs, even if that means throwing a descriptive error.
