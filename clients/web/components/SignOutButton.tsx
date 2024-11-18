import { Button } from "@/components/ui/button";
import { LOGIN_COOKIE_KEY } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { LogOutIcon } from "lucide-react";
import { useRouter } from "next/navigation";

interface Props
  extends Omit<React.ComponentProps<typeof Button>, "onClick" | "type"> {}

export default function SignOutButton({
  children,
  className,
  ...props
}: Props) {
  const { push } = useRouter();

  const handleSignout = () => {
    document.cookie = `${LOGIN_COOKIE_KEY}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
    push("/sign-in");
  };

  return (
    <Button
      variant="outline"
      type="button"
      onClick={handleSignout}
      className={cn("gap-1", className)}
      {...props}
    >
      <LogOutIcon size={16} />
      {children}
    </Button>
  );
}
