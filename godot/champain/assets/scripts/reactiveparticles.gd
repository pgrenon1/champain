extends GPUParticles2D


@export var max_amount = 250

# Called when the node enters the scene tree for the first time.
func _ready():
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	pass


func _on_wds_value_changed(value):
	#self.amount = int(max_amount * value)
	self.amount_ratio = value
