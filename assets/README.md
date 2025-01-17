To create the GIFs do the following:
1. Set the screen resolution to 1280×720 in OS settings
2. Run the [mouse script](../scripts/mouse.main.kts) to generate mouse movements
3. Run the AutoMouse Pro and click *Load* and select the generated `.arf` file from previous step
4. Click *Replay*
5. Use either FFmpeg or Bandicam to record the video:  
   `./ffmpeg.exe -f gdigrab -framerate 30 -offset_x 0 -offset_y 90 -video_size 1280x580 -show_region 1 -i desktop video.mp4`
6. Convert the video to GIF:  
   `./ffmpeg.exe -i video.mp4 -vf "fps=30,split[s0][s1];[s0]palettegen=max_colors=32[p];[s1][p]paletteuse" -loop 0 output.gif`
7. Optimize the GIF using, for example, online tools
