class_name Player extends RigidBody2D

@export var speed = 400
@export var player_id = 0
@export var use_mouse = false

var lap_count = 0
var validated_checkpoints = []

var player_controller

func _ready() -> void:
	pass
	
func _physics_process(delta: float) -> void:
	var player_pointing_angle = ControllerManager.instance.get_player_angle(player_id)
	$debug.global_rotation = player_pointing_angle

	var direction = Vector2.ZERO
	
	var boost = false
	
	if use_mouse:
		direction = position - get_viewport().get_mouse_position()
		direction = direction.normalized()
		
		if Input.is_action_just_pressed("move" + str(player_id+1)):
			boost = true
	else:
		direction = -Vector2.from_angle(player_pointing_angle)
		
		if ControllerManager.instance.get_shake_down(player_id):
			boost = true
	
	if boost:
		apply_impulse(direction * speed)


func validate_checkpoint(checkpoint_id: int):
	if !validated_checkpoints.has(checkpoint_id):
		validated_checkpoints.append(checkpoint_id)
		#print("Player " + str(player_id) + " got checkpoint " + str(checkpoint_id))
