extends Control


# Called when the node enters the scene tree for the first time.
func _ready():
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	pass


func _on_soccer_button_down():
	Global.goto_scene("res://assets/scenes/levels/soccer_1.tscn")

func _on_race_button_down():
	Global.goto_scene("res://assets/scenes/levels/race_1.tscn")
