extends Node

func _ready():
	var music = load("res://assets/scenes/AudioStreamPlayer_Music.tscn").instantiate()
	add_child(music) # Add the music node as a child of the root node
