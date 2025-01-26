class_name Player extends RigidBody2D

@export var speed = 400
@export var player_id = 0
var lap_count = 0
var validated_checkpoints = []

func _ready() -> void:
	pass
	
func _physics_process(delta: float) -> void:
	if Input.is_action_just_pressed("move" + str(player_id)):
		var direction = position - get_viewport().get_mouse_position()
		direction = direction.normalized()
		apply_impulse(direction * speed)

func validate_checkpoint(checkpoint_id: int):
	if !validated_checkpoints.has(checkpoint_id):
		validated_checkpoints.append(checkpoint_id)
		#print("Player " + str(player_id) + " got checkpoint " + str(checkpoint_id))
