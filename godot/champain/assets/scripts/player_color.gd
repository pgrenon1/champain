extends Sprite2D

var colors = [
	"#ffbfbf",
	"#ffedbf",
	"#cfffbf",
	"#bffff1",
	"#bfcfff",
	"#eabfff",
	"#ffbfc0"
]

# Called when the node enters the scene tree for the first time.
func _ready():
	set_self_modulate(Color(colors[self.get_parent().get_parent().player_id], 1.0))
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	pass
