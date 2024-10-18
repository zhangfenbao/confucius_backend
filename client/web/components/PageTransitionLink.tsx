import emitter from "@/lib/eventEmitter";
import Link from "next/link";

type Props = React.ComponentProps<typeof Link>;

export default function PageTransitionLink({ onClick, ...props }: Props) {
  const handleClick = (ev: React.MouseEvent<HTMLAnchorElement>) => {
    emitter.emit("showPageTransitionLoader");
    onClick?.(ev);
  };

  return <Link onClick={handleClick} {...props} />;
}
