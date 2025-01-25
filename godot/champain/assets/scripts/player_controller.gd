extends RigidBody2D

@export var speed = 400
@export var player_id = 0
var validated_checkpoints = []

func _ready() -> void:
	pass
# Called every frame. 'delta' is the elapsed time since the previous frame.
func _physics_process(delta: float) -> void:
	var direction = position - get_viewport().get_mouse_position()
	direction = direction.normalized()
	if Input.is_action_just_pressed("move" + str(player_id)):
		apply_impulse(direction * speed)
	pass

func validate_checkpoint(id: int):
	pass
