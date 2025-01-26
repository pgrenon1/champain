extends AudioStreamPlayer


func play_random_pitch():
	pitch_scale = randf_range(0.9, 1.1)
	play()
