extends RigidBody2D

@export var speed = 400
@export var player_id = 0
# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _physics_process(delta: float) -> void:
	var direction = get_viewport().get_mouse_position() - position
	direction = direction.normalized()
	if Input.is_action_just_pressed("move"):
		apply_impulse(direction * speed)
	#move_and_collide()
	pass
