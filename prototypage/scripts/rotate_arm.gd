extends Sprite2D


# Called when the node enters the scene tree for the first time.
func _ready():
	pass # Replace with function body.

func _process(delta):
	# Get mouse position in global coordinates
	var mouse_pos = get_global_mouse_position()
	
	# Get vector from sprite center to mouse
	var direction = mouse_pos - global_position
	
	# Calculate rotation angle to face mouse
	rotation = direction.angle() - 30
