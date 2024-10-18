import modal
from dotenv import load_dotenv
from webapp.main import app as webapp

app = modal.App("sesame")

env_vars = load_dotenv()

image = (
    modal.Image.debian_slim(python_version="3.12")
    .pip_install_from_requirements("webapp/requirements.txt")
    .pip_install_from_requirements("bots/requirements.txt")
)


@app.function(
    image=image,
    cpu=1.0,
    secrets=[modal.Secret.from_dotenv()],
    keep_warm=1,
    enable_memory_snapshot=True,
    max_inputs=1,  # Do not reuse instances as the pipeline needs to be restarted
    retries=0,
)
def launch_bot_modal(*args, **kwargs):
    from bots.voice.bot import _voice_bot_process

    _voice_bot_process(*args, **kwargs)


@app.function(
    image=image,
    secrets=[modal.Secret.from_dotenv(), modal.Secret.from_dict({"MODAL_ENV": "1"})],
    keep_warm=1,
)
@modal.asgi_app()
def api():
    return webapp
