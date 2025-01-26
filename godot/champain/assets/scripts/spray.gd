extends Node2D

@onready var particles = $GPUParticles2D

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	toggle_particles(false)
	
# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	if Input.is_action_just_pressed("ui_accept"):  # Will only trigger once when spacebar is pressed
		toggle_particles(!particles.emitting)

func toggle_particles(value: bool) -> void:
	particles.emitting = value
	
func set_amount_ratio(ratio: float):
	particles.amount_ratio = ratio
