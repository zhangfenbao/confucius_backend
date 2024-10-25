import base64
import secrets
import shutil
from pathlib import Path

import click

env_example = Path("env.example")
env_file = Path(".env")


@click.group()
def cli():
    """CLI tool for project setup and management."""
    pass


@cli.command()
@click.pass_context
def init(ctx):
    """Initialize a new project and Sesame environment"""

    if env_file.exists():
        click.echo(click.style("Warning: Project .env file already exists.", fg="red"))
        choice = click.prompt(
            click.style(
                "\nWould you like to proceed? This will overwrite your existing settings", fg="blue"
            ),
            type=click.Choice(
                ["Y", "n"],
                case_sensitive=False,
            ),
        )
        if choice.lower() == "n":
            return

    choice = click.prompt(
        click.style(
            "\nWould you like to generate a random app secret or provide your own?", fg="blue"
        ),
        type=click.Choice(
            ["Y", "n"],
            case_sensitive=False,
        ),
    )

    if choice.lower() == "n":
        app_secret = click.prompt(
            click.style("Please enter your app secret", fg="blue"), type=str, hide_input=True
        )
        click.echo("Using provided app secret.")
    else:
        # Generate 32 random bytes and encode as base64, matching openssl rand -base64 32
        random_bytes = secrets.token_bytes(32)
        app_secret = base64.b64encode(random_bytes).decode("utf-8")
        click.echo("Generated random app secret.")

    # ---- Handle env file setup

    try:
        # Copy the example file first
        shutil.copy2(env_example, env_file)
        click.echo("Successfully created .env file from env.example")

        # Read the current contents
        with open(env_file, "r") as f:
            env_contents = f.readlines()

        # Update or add the SESAME_APP_SECRET
        secret_found = False
        for i, line in enumerate(env_contents):
            if line.startswith("SESAME_APP_SECRET="):
                env_contents[i] = f'SESAME_APP_SECRET="{app_secret}"\n'
                secret_found = True
                break

        if not secret_found:
            env_contents.append(f"SESAME_APP_SECRET={app_secret}\n")

        # Write back the updated contents
        with open(env_file, "w") as f:
            f.writelines(env_contents)

    except Exception as e:
        click.echo(click.style(f"\nError creating .env file: {str(e)}", fg="red"))

    choice = click.prompt(
        click.style("\nWould you like to configure your database", fg="blue"),
        type=click.Choice(
            ["Y", "n"],
            case_sensitive=False,
        ),
    )

    if choice.lower() == "n":
        click.echo("Skipping database configuration. You can do this later by running init_db")
    else:
        ctx.invoke(init_db)

    click.echo(click.style("\nProject successfully initialized!", fg="green"))


@cli.command()
def init_db():
    print("SESAME DB")


if __name__ == "__main__":
    cli()
