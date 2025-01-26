# ProjectileMovement.gd
extends Sprite2D

var velocity = Vector2.ZERO
var launch_speed = 0.0

func launch():
	velocity = Vector2.UP.rotated(global_rotation)

func _physics_process(delta):
	global_position += velocity * launch_speed
